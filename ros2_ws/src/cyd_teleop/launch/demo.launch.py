"""Lanza turtlesim y el driver táctil (CYD por serial, o app Android por UDP o Bluetooth)."""

from launch import LaunchDescription
from launch.actions import DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration
from launch_ros.actions import Node
from launch_ros.parameter_descriptions import ParameterValue


def generate_launch_description():
    port = LaunchConfiguration('port')
    transport = LaunchConfiguration('transport')
    udp_port = LaunchConfiguration('udp_port')

    return LaunchDescription([
        DeclareLaunchArgument(
            'port', default_value='/dev/esp32_tactil',
            description='Puerto serial (p. ej. /dev/ttyUSB0 o /dev/rfcomm0)'),
        DeclareLaunchArgument(
            'transport', default_value='serial',
            description="De dónde llegan las letras: 'serial' o 'udp'"),
        DeclareLaunchArgument(
            'udp_port', default_value='5005',
            description='Puerto UDP donde escuchar (solo con transport:=udp)'),

        Node(package='turtlesim', executable='turtlesim_node', name='turtlesim'),

        Node(
            package='cyd_teleop', executable='driver_tactil', name='driver_tactil',
            output='screen',
            parameters=[{
                'port': port,
                'transport': transport,
                'udp_port': ParameterValue(udp_port, value_type=int),
            }],
        ),
    ])
