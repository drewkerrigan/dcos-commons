import dcos.http
import dcos.marathon
import pytest
import shakedown

from tests.test_utils import (
    DEFAULT_TASK_COUNT,
    PACKAGE_NAME,
    check_health,
    get_marathon_config,
    get_deployment_plan,
    install,
    marathon_api_url,
    request,
    uninstall,
    spin
)


def setup_module(module):
    uninstall()

    install()

    check_health()


def teardown_module(module):
    uninstall()


@pytest.mark.sanity
def test_install_worked():
    pass


@pytest.mark.sanity
def test_bump_hello_cpus():
    check_health()
    hello_ids = get_task_ids('hello')
    print('hello ids: ' + str(hello_ids))

    config = get_marathon_config()
    cpus = float(config['env']['HELLO_CPUS'])
    config['env']['HELLO_CPUS'] = str(cpus + 0.1)
    r = request(
        dcos.http.put,
        marathon_api_url('apps/' + PACKAGE_NAME),
        json=config)

    tasks_updated('hello', hello_ids)

    check_health()


@pytest.mark.sanity
def test_bump_hello_nodes():
    check_health()

    hello_ids = get_task_ids('hello')
    print('hello ids: ' + str(hello_ids))

    config = get_marathon_config()
    nodeCount = int(config['env']['HELLO_COUNT']) + 1
    config['env']['HELLO_COUNT'] = str(nodeCount)
    r = request(
        dcos.http.put,
        marathon_api_url('apps/' + PACKAGE_NAME),
        json=config)

    check_health(DEFAULT_TASK_COUNT + 1)
    tasks_not_updated('hello', hello_ids)


def test_lock():
    '''This test verifies that a second scheduler fails to startup when
    an existing scheduler is running.  Without locking, the scheduler
    would fail during registration, but after writing its config to ZK.
    So in order to verify that the scheduler fails immediately, we ensure
    that the ZK config state is unmodified.'''

    ZK_PATH = "dcos-service-{}/ConfigTarget".format(PACKAGE_NAME)

    # Get ZK state from running framework
    zk_config_old = shakedown.get_zk_node_data(ZK_PATH)

    # Install second framework
    options = {"service": {"name": "hello-world-lock"}, "hello": {"count": 2}}
    install(additional_options=options, wait_for_completion=False)

    # Wait for second scheduler to terminate
    client = dcos.marathon.create_client()
    tasks = client.get_tasks("/hello-world-lock")
    task_id = tasks[0]["id"]
    shakedown.wait_for_task_completion(task_id)

    # Verify ZK is unchanged
    zk_config_new = get_zk_node_data("ZK_PATH")
    assert zk_config_old == zk_config_new

    uninstall()


def get_task_ids(prefix):
    tasks = shakedown.get_service_tasks(PACKAGE_NAME)
    prefixed_tasks = [t for t in tasks if t['name'].startswith(prefix)]
    task_ids = [t['id'] for t in prefixed_tasks]
    return task_ids


def tasks_updated(prefix, old_task_ids):
    def fn():
        try:
            return get_task_ids(prefix)
        except dcos.errors.DCOSHTTPException:
            return []

    def success_predicate(task_ids):
        print('Old task ids: ' + str(old_task_ids))
        print('New task ids: ' + str(task_ids))
        success = True

        for id in task_ids:
            print('Checking ' + id)
            if id in old_task_ids:
                success = False

        if not len(task_ids) >= len(old_task_ids):
            success = False

        print('Waiting for update to ' + prefix)
        return (
            success,
            'Task type:' + prefix + ' not updated'
        )

    return spin(fn, success_predicate)


def tasks_not_updated(prefix, old_task_ids):
    def fn():
        try:
            return get_task_ids(prefix)
        except dcos.errors.DCOSHTTPException:
            return []

    def success_predicate(task_ids):
        print('Old task ids: ' + str(old_task_ids))
        print('New task ids: ' + str(task_ids))
        success = True

        for id in old_task_ids:
            print('Checking ' + id)
            if id not in task_ids:
                success = False

        if not len(task_ids) >= len(old_task_ids):
            success = False

        print('Determining no update occurred for ' + prefix)
        return (
            success,
            'Task type:' + prefix + ' not updated'
        )

    return spin(fn, success_predicate)
